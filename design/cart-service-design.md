# Cart Service — Backend Responsibilities & Functional Requirements
## Purpose :
The Cart Service is responsible for managing a customer's shopping cart throughout the shopping journey.
- ### Its primary responsibility is:
    - Maintain the current set of books a user intends to purchase, along with quantities and cart-level information, until the cart is converted into an order or cleared.
  
## Position in the System Architecture:
                        Angular Application
                                |
                                v
                            API Gateway
                                |
                                v
                         +--------------+
                         | Cart Service |
                         +--------------+
                           |    |    |
                           |    |    |
                           v    v    v
                       Cart-DB Redis Catalog

## Core Responsibilities

The Cart Service should have the following major responsibilities:

1. Create and maintain carts
2. Add books to cart
3. Update item quantities
4. Remove items from cart
5. Clear cart
6. Retrieve the current cart
7. Calculate cart quantities
8. Calculate cart subtotal
9. Validate cart operations
10. Handle duplicate book additions
11. Validate basic book availability
12. Maintain price information required for cart calculations
13. Handle cart ownership and authorization
14. Support guest carts, if required
15. Merge guest cart with user cart after login
16. Handle cart expiration if required
17. Protect against invalid quantities
18. Handle concurrent cart updates
19. Prepare cart for checkout
20. Transfer cart information to Order Service
21. Clear/close cart after successful order creation
22. Publish relevant cart events
23. Provide observability and audit information
24. Maintain cart data independently of Catalog Service.

## Cart Lifecycle:
                 +----------------+
                 |  No Cart       |
                 +-------+--------+
                         |
                    Add Book
                         |
                         v
                 +----------------+
                 | Active Cart    |
                 +-------+--------+
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
         Add Item    Update Qty   Remove Item
             |           |           |
             +-----------+-----------+
                         |
                         v
                 +----------------+
                 | Ready for      |
                 | Checkout       |
                 +-------+--------+
                         |
                      Checkout
                         |
                         v
                 +----------------+
                 | Order Created  |
                 +-------+--------+
                         |
                         v
                 +----------------+
                 | Cart Completed |
                 +----------------+    


 - ## Database Schema
 - ### Tables
 - **Users**
 - | Column Name | Data Type | Description |
 - |-------------|-----------|-------------|
 - | user_id | UUID | Primary key, unique identifier for each user |
 - | email | VARCHAR(255) | User's email address, unique |
 - | created_at | TIMESTAMP | Timestamp when the user was created |
 - **Carts**
 - | Column Name | Data Type | Description |
 - |-------------|-----------|-------------|
 - | cart_id | UUID | Primary key, unique identifier for each cart |
 - | user_id | UUID | Foreign key referencing Users(user_id), identifies the owner of the cart |
 - | created_at | TIMESTAMP | Timestamp when the cart was created |
 - | updated_at | TIMESTAMP | Timestamp when the cart was last updated |
 - **Cart_Items**
 - | Column Name | Data Type | Description |
 - |-------------|-----------|-------------|
 - | cart_item_id | UUID | Primary key, unique identifier for each cart item |
 - | cart_id | UUID | Foreign key referencing Carts(cart_id), identifies the cart to which the item belongs |
 - | product_id | UUID | Foreign key referencing Products(product_id), identifies the product added to the cart |
 - | quantity | INT | Quantity of the product in the cart |
 - | price | DECIMAL(10, 2) | Price of the product at the time it was added to the cart |
 - | created_at | TIMESTAMP | Timestamp when the cart item was added |
 - | updated_at | TIMESTAMP | Timestamp when the cart item was last updated |

## API Endpoints: 

| Method   | Endpoint                    | Responsibility                 |
| -------- | --------------------------- | ------------------------------ |
| `GET`    | `/api/carts`                | Get current user's cart        |
| `POST`   | `/api/carts/items`          | Add item                       |
| `PUT`    | `/api/carts/items/{bookId}` | Update quantity                |
| `DELETE` | `/api/carts/items/{bookId}` | Remove item                    |
| `DELETE` | `/api/carts`                | Clear cart                     |
| `GET`    | `/api/carts/count`          | Get cart item count            |
| `POST`   | `/api/carts/validate`       | Validate cart before checkout  |
| `POST`   | `/api/carts/checkout`       | Prepare/lock cart for checkout |

### Example API Request/Response:

**Add Item to Cart**
- **Request:**
```http
POST /api/carts/items
Content-Type: application/json
{
  "product_id": "123e4567-e89b-12d3-a456-426614174000",
  "quantity": 2
}
```
- **Processing:**

        - Validate the request payload.
        - Validate the User ID from the authentication token.
        - Validate the product ID and quantity.
        - Check if the product exists in the catalog.
        - If the cart does not exist for the user, create a new cart.
        - Add the item to the cart or update the quantity if it already exists.
        - Calculate the new subtotal and total quantity.
        - Return the updated cart information in the response.
- **Response:**
```http
HTTP/1.1 201 Created
Content-Type: application/json
{
  "cart_id": "987e6543-e21b-12d3-a456-426614174000",
  "items": [
    {
      "product_id": "123e4567-e89b-12d3-a456-426614174000",
      "quantity": 2,
      "price": 19.99
    }
  ],
  "total_quantity": 2,
  "subtotal": 39.98
}
```
### Error Handling:
- **Invalid Product ID:**
- **Response:**
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json
{
  "error": "Invalid product ID"
}
```
- **Cart Service specific Response Codes and Messages:**
- | Status Code | Message | Description |
- | ----------- | ------- | ----------- |
- | 400         | Invalid product ID | The provided product ID does not exist in the catalog. |
- | 400         | Invalid quantity | The provided quantity is not valid (e.g., negative or zero). |
- | 404         | Cart not found | The cart for the user does not exist. |
- | 409         | Item already in cart | The item is already in the cart, and the request was to add it again without updating the quantity. |
- | 500         | Internal server error | An unexpected error occurred on the server. |
- | 401         | Unauthorized | The user is not authenticated or does not have permission to access the cart. |
- | 403         | Forbidden | The user does not have permission to modify the cart (e.g., trying to modify another user's cart). |
- | 422         | Cart validation failed | The cart failed validation checks before checkout (e.g., items out of stock). |
- | 503         | Service unavailable | The cart service is temporarily unavailable, please try again later. |

## Complete Responsibility Summary:

                    CART SERVICE
                         |
       +-----------------+------------------+
       |                 |                  |
       v                 v                  v
    Cart CRUD         Validation         Calculation
    |                 |                  |
    |                 |                  |
    v                 v                  v
    Add item          Stock check          Subtotal
    Update qty        Price validation     Item count
    Remove item       Ownership            Product count
    Clear cart        Quantity rules
    |
    +-----------------+
                    |
                    v
                    User & Security
                    |
                    v
                    Checkout Support
                    |
                    v
                    Events
                    |
                    v
                    Persistence/Cache
                    |
                    v
                    Observability
